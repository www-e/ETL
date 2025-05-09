/**
 * Results Table Component
 * Handles displaying processed ETL data in a table
 */
class ResultsTableManager {
    constructor() {
        // DOM elements
        this.resultsContent = document.getElementById('resultsContent');
        this.resultsLoader = document.getElementById('resultsLoader');
    }
    
    /**
     * Load processed data from API
     */
    async loadData() {
        try {
            // Show loader
            this.showLoader();
            
            // Get processed data from API
            const data = await apiService.getAllData();
            
            // Render results table
            this.renderResultsTable(data);
        } catch (error) {
            console.error('Error loading results data:', error);
            this.resultsContent.innerHTML = `
                <div class="error-message">
                    <p>Error loading results: ${error.message}</p>
                </div>
            `;
        } finally {
            // Hide loader
            this.hideLoader();
        }
    }
    
    /**
     * Render processed data as a table
     * @param {Array} data - The processed data
     */
    renderResultsTable(data) {
        if (!data || data.length === 0) {
            this.resultsContent.innerHTML = '<p class="no-data-message">No processed data available</p>';
            return;
        }
        
        // Create table HTML
        let tableHtml = '<div class="table-responsive"><table class="data-table">';
        
        // Add header row
        tableHtml += '<thead><tr>';
        // Define columns to display (not all fields)
        const columns = [
            { key: 'id', label: 'ID' },
            { key: 'firstName', label: 'First Name' },
            { key: 'lastName', label: 'Last Name' },
            { key: 'email', label: 'Email' },
            { key: 'birthDate', label: 'Birth Date' },
            { key: 'age', label: 'Age' },
            { key: 'city', label: 'City' },
            { key: 'country', label: 'Country' },
            { key: 'salary', label: 'Salary' },
            { key: 'taxRate', label: 'Tax Rate' },
            { key: 'netSalary', label: 'Net Salary' },
            { key: 'processingStatus', label: 'Status' }
        ];
        
        columns.forEach(column => {
            tableHtml += `<th>${column.label}</th>`;
        });
        tableHtml += '</tr></thead>';
        
        // Add data rows
        tableHtml += '<tbody>';
        data.forEach(row => {
            // Add status class for row
            const rowClass = row.processingStatus === 'VALID' ? 'valid-row' : 'invalid-row';
            tableHtml += `<tr class="${rowClass}">`;
            
            columns.forEach(column => {
                let value = row[column.key];
                
                // Format values based on column type
                switch (column.key) {
                    case 'birthDate':
                        value = value ? formatters.date(value) : '';
                        break;
                    case 'salary':
                    case 'netSalary':
                        value = formatters.currency(value);
                        break;
                    case 'taxRate':
                        value = formatters.percentage(value);
                        break;
                    case 'processingStatus':
                        const statusClass = value === 'VALID' ? 'status-success' : 'status-error';
                        value = `<span class="${statusClass}">${value}</span>`;
                        break;
                    default:
                        value = value !== null && value !== undefined ? value : '';
                }
                
                tableHtml += `<td>${value}</td>`;
            });
            
            tableHtml += '</tr>';
        });
        tableHtml += '</tbody></table></div>';
        
        // Add table info
        tableHtml += `<div class="table-info">Showing ${data.length} records</div>`;
        
        // Update results content
        this.resultsContent.innerHTML = tableHtml;
        
        // Add row click event for details
        this.addRowClickEvents(data);
    }
    
    /**
     * Add click events to table rows for showing details
     * @param {Array} data - The processed data
     */
    addRowClickEvents(data) {
        const rows = this.resultsContent.querySelectorAll('tbody tr');
        
        rows.forEach((row, index) => {
            row.addEventListener('click', () => {
                this.showRowDetails(data[index]);
            });
        });
    }
    
    /**
     * Show detailed information for a row
     * @param {Object} rowData - The row data
     */
    showRowDetails(rowData) {
        // Create modal for details
        const modal = document.createElement('div');
        modal.className = 'modal active';
        
        // Format validation messages
        const validationMessages = rowData.validationMessages ? 
            rowData.validationMessages.split(';').map(msg => `<li>${msg.trim()}</li>`).join('') :
            '<li>No validation messages</li>';
        
        // Create modal content
        modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h3><i class="fas fa-info-circle"></i> Record Details</h3>
                    <span class="close-modal">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="record-details">
                        <h4><i class="fas fa-user-circle"></i> Personal Information</h4>
                        <div class="details-grid">
                            <div class="detail-item">
                                <span class="detail-label">ID:</span>
                                <span class="detail-value">${rowData.id || 'N/A'}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Full Name:</span>
                                <span class="detail-value">${rowData.fullName || `${rowData.firstName} ${rowData.lastName}` || 'N/A'}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Email:</span>
                                <span class="detail-value">${rowData.email || 'N/A'}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Birth Date:</span>
                                <span class="detail-value">${rowData.birthDate ? formatters.date(rowData.birthDate) : 'N/A'}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Age:</span>
                                <span class="detail-value">${rowData.age || 'N/A'}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Phone:</span>
                                <span class="detail-value">${rowData.phoneNumber || 'N/A'}</span>
                            </div>
                        </div>
                        
                        <h4><i class="fas fa-map-marker-alt"></i> Location</h4>
                        <div class="details-grid">
                            <div class="detail-item">
                                <span class="detail-label">Address:</span>
                                <span class="detail-value">${rowData.address || 'N/A'}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">City:</span>
                                <span class="detail-value">${rowData.city || 'N/A'}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Country:</span>
                                <span class="detail-value">${rowData.country || 'N/A'}</span>
                            </div>
                        </div>
                        
                        <h4><i class="fas fa-money-bill-wave"></i> Financial Information</h4>
                        <div class="details-grid">
                            <div class="detail-item">
                                <span class="detail-label">Salary:</span>
                                <span class="detail-value">${formatters.currency(rowData.salary)}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Tax Rate:</span>
                                <span class="detail-value">${formatters.percentage(rowData.taxRate)}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Net Salary:</span>
                                <span class="detail-value">${formatters.currency(rowData.netSalary)}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Dependents:</span>
                                <span class="detail-value">${rowData.dependents || '0'}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Dependent Allowance:</span>
                                <span class="detail-value">${formatters.currency(rowData.dependentAllowance)}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Total Deductions:</span>
                                <span class="detail-value">${formatters.currency(rowData.totalDeductions)}</span>
                            </div>
                        </div>
                        
                        <h4><i class="fas fa-cogs"></i> Processing Information</h4>
                        <div class="details-grid">
                            <div class="detail-item">
                                <span class="detail-label">Status:</span>
                                <span class="detail-value ${rowData.processingStatus === 'VALID' ? 'status-success' : 'status-error'}">${rowData.processingStatus || 'N/A'}</span>
                            </div>
                            <div class="detail-item">
                                <span class="detail-label">Processed At:</span>
                                <span class="detail-value">${rowData.processedAt ? formatters.dateTime(rowData.processedAt) : 'N/A'}</span>
                            </div>
                        </div>
                        
                        <h4><i class="fas fa-clipboard-check"></i> Validation Messages</h4>
                        <ul class="validation-messages">
                            ${validationMessages}
                        </ul>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary close-details-btn">Close</button>
                </div>
            </div>
        `;
        
        // Add modal to body
        document.body.appendChild(modal);
        
        // Add close event
        const closeButtons = modal.querySelectorAll('.close-modal, .close-details-btn');
        closeButtons.forEach(button => {
            button.addEventListener('click', () => {
                document.body.removeChild(modal);
            });
        });
        
        // Close when clicking outside
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                document.body.removeChild(modal);
            }
        });
    }
    
    /**
     * Show the loader
     */
    showLoader() {
        this.resultsLoader.classList.remove('hidden');
        this.resultsContent.innerHTML = '';
    }
    
    /**
     * Hide the loader
     */
    hideLoader() {
        this.resultsLoader.classList.add('hidden');
    }
}

// Initialize results table when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.resultsTable = new ResultsTableManager();
});
