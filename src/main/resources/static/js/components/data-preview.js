/**
 * Data Preview Component
 * Handles previewing data from uploaded files
 */
class DataPreviewManager {
    constructor() {
        // DOM elements
        this.previewContent = document.getElementById('previewContent');
        this.previewLoader = document.getElementById('previewLoader');
        this.previewCard = document.getElementById('previewCard');
    }
    
    /**
     * Preview a file's contents
     * @param {File} file - The file to preview
     */
    async previewFile(file) {
        try {
            // Show preview card if hidden
            if (this.previewCard.classList.contains('hidden')) {
                this.previewCard.classList.remove('hidden');
                // Add a small animation
                this.previewCard.classList.add('fade-in');
                // Scroll to preview card
                setTimeout(() => {
                    this.previewCard.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }, 100);
            }
            
            // Show loader
            this.showLoader();
            
            // Get preview data from API
            const previewData = await apiService.previewFile(file);
            
            // Render preview table
            this.renderPreviewTable(previewData);
        } catch (error) {
            console.error('Error previewing file:', error);
            this.previewContent.innerHTML = `
                <div class="error-message">
                    <p>Error previewing file: ${error.message}</p>
                </div>
            `;
        } finally {
            // Hide loader
            this.hideLoader();
        }
    }
    
    /**
     * Render preview data as a table
     * @param {Array} data - The preview data
     */
    renderPreviewTable(data) {
        if (!data || data.length === 0) {
            this.previewContent.innerHTML = '<p class="no-data-message">No data to preview</p>';
            return;
        }
        
        // Get column headers from first row
        const headers = Object.keys(data[0]);
        
        // Create table HTML
        let tableHtml = '<div class="table-responsive"><table class="data-table">';
        
        // Add header row
        tableHtml += '<thead><tr>';
        headers.forEach(header => {
            tableHtml += `<th>${header}</th>`;
        });
        tableHtml += '</tr></thead>';
        
        // Add data rows
        tableHtml += '<tbody>';
        data.forEach(row => {
            tableHtml += '<tr>';
            headers.forEach(header => {
                const value = row[header] !== null && row[header] !== undefined ? row[header] : '';
                tableHtml += `<td>${value}</td>`;
            });
            tableHtml += '</tr>';
        });
        tableHtml += '</tbody></table></div>';
        
        // Update preview content
        this.previewContent.innerHTML = tableHtml;
    }
    
    /**
     * Show the loader
     */
    showLoader() {
        this.previewLoader.classList.remove('hidden');
        this.previewContent.innerHTML = '';
    }
    
    /**
     * Hide the loader
     */
    hideLoader() {
        this.previewLoader.classList.add('hidden');
    }
}

// Initialize data preview when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.dataPreview = new DataPreviewManager();
});
