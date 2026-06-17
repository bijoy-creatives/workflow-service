// State Management
let workflows = [];
let executions = [];
let selectedWorkflowId = null;
let activeExecutionId = null;
let pollingInterval = null;

// DOM Elements
const workflowList = document.getElementById('workflow-list');
const executionList = document.getElementById('execution-list');
const activeTitle = document.getElementById('active-title');
const activeSubtitle = document.getElementById('active-subtitle');
const btnExecute = document.getElementById('btn-execute');
const flowDiagram = document.getElementById('flow-diagram');
const contextViewer = document.getElementById('context-viewer');
const jsonViewer = document.getElementById('json-viewer');
const executionBadge = document.getElementById('execution-badge');

const createModal = document.getElementById('create-modal');
const btnShowCreate = document.getElementById('btn-show-create');
const btnCloseModal = document.getElementById('btn-close-modal');
const createForm = document.getElementById('create-workflow-form');

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    loadWorkflows();
    loadExecutions();

    // Event Listeners
    btnExecute.addEventListener('click', triggerExecution);
    btnShowCreate.addEventListener('click', () => createModal.showModal());
    btnCloseModal.addEventListener('click', () => createModal.close());
    
    createForm.addEventListener('submit', handleCreateWorkflow);
});

// Load Workflow Definitions from API
async function loadWorkflows() {
    try {
        const response = await fetch('/api/workflows');
        workflows = await response.json();
        renderWorkflows();
    } catch (error) {
        console.error('Error loading workflows:', error);
        workflowList.innerHTML = '<div class="empty-state">Failed to load definitions</div>';
    }
}

// Load Executions from API
async function loadExecutions() {
    try {
        const response = await fetch('/api/executions');
        executions = await response.json();
        renderExecutions();
    } catch (error) {
        console.error('Error loading executions:', error);
    }
}

// Render workflows sidebar
function renderWorkflows() {
    if (workflows.length === 0) {
        workflowList.innerHTML = '<div class="empty-state">No workflows defined</div>';
        return;
    }

    workflowList.innerHTML = '';
    workflows.forEach(wf => {
        const button = document.createElement('button');
        button.className = `item-btn ${selectedWorkflowId === wf.workflowId ? 'active' : ''}`;
        button.innerHTML = `
            <span class="item-title">${wf.workflowId}</span>
            <span class="item-desc">${wf.steps.length} steps configured</span>
        `;
        button.addEventListener('click', () => selectWorkflow(wf.workflowId));
        workflowList.appendChild(button);
    });
}

// Render executions sidebar
function renderExecutions() {
    // Sort by started time desc
    const sorted = [...executions].sort((a, b) => new Date(b.startedAt) - new Date(a.startedAt));

    if (sorted.length === 0) {
        executionList.innerHTML = '<div class="empty-state">No executions run yet</div>';
        return;
    }

    executionList.innerHTML = '';
    sorted.forEach(exec => {
        const button = document.createElement('button');
        button.className = `item-btn ${activeExecutionId === exec.executionId ? 'active' : ''}`;
        
        const startedTime = new Date(exec.startedAt).toLocaleTimeString();
        button.innerHTML = `
            <span class="item-title">${exec.workflowId}</span>
            <span class="item-desc">ID: ${exec.executionId.substring(0, 8)}...</span>
            <div class="status-row">
                <span class="dot dot-${exec.status.toLowerCase()}"></span>
                <span class="item-desc" style="text-transform: capitalize;">${exec.status.toLowerCase()} (step ${exec.currentStep}/${exec.steps.length})</span>
            </div>
        `;
        button.addEventListener('click', () => selectExecution(exec.executionId));
        executionList.appendChild(button);
    });
}

// Select a workflow definition
function selectWorkflow(workflowId) {
    selectedWorkflowId = workflowId;
    activeExecutionId = null;
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }

    const wf = workflows.find(w => w.workflowId === workflowId);
    activeTitle.textContent = wf.workflowId;
    activeSubtitle.textContent = `Definition is ready. Click "Execute" to start running.`;
    
    btnExecute.removeAttribute('disabled');
    renderWorkflows();
    renderExecutions();
    
    // Draw empty/pending states for the definition steps
    drawPendingWorkflow(wf);
    updateJsonViewer(wf);
    updateContextViewer({});
    executionBadge.textContent = 'Ready';
    executionBadge.className = 'badge';
}

// Select a specific execution run
async function selectExecution(executionId) {
    activeExecutionId = executionId;
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }

    renderExecutions();
    await pollExecutionState();

    const currentExec = executions.find(e => e.executionId === executionId);
    if (currentExec && (currentExec.status === 'RUNNING' || currentExec.status === 'PENDING')) {
        startPolling(executionId);
    }
}

// Draw the pending steps for a selected definition (before execution)
function drawPendingWorkflow(workflow) {
    flowDiagram.innerHTML = '';
    workflow.steps.forEach((step, idx) => {
        // Step node
        const node = document.createElement('div');
        node.className = 'step-node';
        node.innerHTML = `
            <span class="step-name">${step.name}</span>
            <span class="step-status-badge">Pending</span>
        `;
        flowDiagram.appendChild(node);

        // Arrow connector (except for the last step)
        if (idx < workflow.steps.length - 1) {
            const connector = document.createElement('div');
            connector.className = 'step-connector';
            flowDiagram.appendChild(connector);
        }
    });
}

// Trigger running the selected workflow
async function triggerExecution() {
    if (!selectedWorkflowId) return;

    btnExecute.setAttribute('disabled', 'true');
    executionBadge.textContent = 'Initializing...';
    executionBadge.className = 'badge badge-running';

    try {
        const response = await fetch(`/api/workflows/${selectedWorkflowId}/execute`, {
            method: 'POST'
        });
        
        if (!response.ok) throw new Error('Failed to start execution');
        
        const execution = await response.json();
        activeExecutionId = execution.executionId;
        
        // Add to local state & refresh sidebar
        executions.push(execution);
        renderExecutions();
        
        // Update title details
        activeSubtitle.textContent = `Running execution ID: ${execution.executionId}`;
        
        // Start live tracking
        renderExecutionDetails(execution);
        startPolling(execution.executionId);
    } catch (error) {
        console.error('Execution failed:', error);
        executionBadge.textContent = 'Start Failed';
        executionBadge.className = 'badge badge-failed';
        btnExecute.removeAttribute('disabled');
    }
}

// Poll state for active execution
function startPolling(executionId) {
    if (pollingInterval) clearInterval(pollingInterval);
    pollingInterval = setInterval(async () => {
        const finished = await pollExecutionState();
        if (finished) {
            clearInterval(pollingInterval);
            pollingInterval = null;
            btnExecute.removeAttribute('disabled');
            loadExecutions(); // reload full list on completion
        }
    }, 1000);
}

// Fetch execution state and update UI. Returns true if execution is finished.
async function pollExecutionState() {
    if (!activeExecutionId) return true;

    try {
        const response = await fetch(`/api/executions/${activeExecutionId}`);
        if (!response.ok) return true;

        const execution = await response.json();
        
        // Update execution in our list
        const idx = executions.findIndex(e => e.executionId === execution.executionId);
        if (idx !== -1) {
            executions[idx] = execution;
        } else {
            executions.push(execution);
        }

        renderExecutionDetails(execution);
        
        // Return true if state is terminal
        const terminalStates = ['COMPLETED', 'FAILED', 'SUSPENDED'];
        return terminalStates.includes(execution.status);
    } catch (error) {
        console.error('Error polling execution:', error);
        return true;
    }
}

// Render the details of a running or finished execution
function renderExecutionDetails(execution) {
    // 1. Badge status
    executionBadge.textContent = execution.status;
    executionBadge.className = `badge badge-${execution.status.toLowerCase()}`;
    
    // 2. Main title & subtitle
    activeTitle.textContent = execution.workflowId;
    activeSubtitle.textContent = `Execution ID: ${execution.executionId}`;

    // 3. Draw step graph
    flowDiagram.innerHTML = '';
    execution.steps.forEach((step, idx) => {
        // Node container
        const node = document.createElement('div');
        node.className = `step-node status-${step.status.toLowerCase()}`;
        node.innerHTML = `
            <span class="step-name">${step.name}</span>
            <span class="step-status-badge">${step.status.toLowerCase()}</span>
        `;
        flowDiagram.appendChild(node);

        // Arrow connector
        if (idx < execution.steps.length - 1) {
            const nextStep = execution.steps[idx + 1];
            const connector = document.createElement('div');
            const isCompleted = step.status === 'COMPLETED';
            connector.className = `step-connector ${isCompleted ? 'completed' : ''}`;
            flowDiagram.appendChild(connector);
        }
    });

    // 4. Update JSON viewers
    updateJsonViewer(execution);
    updateContextViewer(execution.context || {});
}

// Update the bottom JSON panel
function updateJsonViewer(data) {
    jsonViewer.innerHTML = `<pre><code class="language-json">${JSON.stringify(data, null, 2)}</code></pre>`;
}

// Update the right-side Context panel
function updateContextViewer(data) {
    contextViewer.innerHTML = `<pre><code class="language-json">${JSON.stringify(data, null, 2)}</code></pre>`;
}

// Handle Form Submission for Creating a new Workflow definition
async function handleCreateWorkflow(e) {
    e.preventDefault();
    const workflowId = document.getElementById('workflow-id-input').value.trim();
    const stepsJsonText = document.getElementById('workflow-json-input').value.trim();

    try {
        const steps = JSON.parse(stepsJsonText);
        
        if (!Array.isArray(steps)) {
            throw new Error('Steps must be a JSON Array');
        }

        const payload = {
            workflowId: workflowId,
            steps: steps
        };

        const response = await fetch('/api/workflows', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error(`Server returned status ${response.status}`);
        }

        const newWf = await response.json();
        
        // Reset form & close modal
        createForm.reset();
        createModal.close();

        // Refresh list and select the newly created workflow
        await loadWorkflows();
        selectWorkflow(newWf.workflowId);

    } catch (error) {
        alert(`Failed to save workflow: ${error.message}`);
    }
}
