/**
 * File Upload Component
 * Handles file selection, drag and drop, and upload functionality
 */
class FileUploadManager {
    constructor() {
        // DOM elements
        this.dropArea = document.getElementById('dropArea');
        this.fileInput = document.getElementById('fileInput');
        this.fileInfo = document.getElementById('fileInfo');
        this.previewBtn = document.getElementById('previewBtn');
        this.processBtn = document.getElementById('processBtn');
        
        // State
        this.selectedFile = null;
        
        this.init();
    }
    
    /**
     * Initialize file upload functionality
     */
    init() {
        // File input change event
        this.fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileSelection(e.target.files[0]);
            }
        });
        
        // Drag and drop events
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            this.dropArea.addEventListener(eventName, this.preventDefaults, false);
        });
        
        ['dragenter', 'dragover'].forEach(eventName => {
            this.dropArea.addEventListener(eventName, () => {
                this.dropArea.classList.add('active');
            }, false);
        });
        
        ['dragleave', 'drop'].forEach(eventName => {
            this.dropArea.addEventListener(eventName, () => {
                this.dropArea.classList.remove('active');
            }, false);
        });
        
        this.dropArea.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            if (dt.files.length > 0) {
                this.handleFileSelection(dt.files[0]);
            }
        }, false);
        
        // Button click events
        this.previewBtn.addEventListener('click', () => {
            if (this.selectedFile) {
                window.dataPreview.previewFile(this.selectedFile);
            }
        });
        
        this.processBtn.addEventListener('click', () => {
            if (this.selectedFile) {
                this.uploadFile();
            }
        });
    }
    
    /**
     * Prevent default browser behavior for drag events
     * @param {Event} e - The event object
     */
    preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }
    
    /**
     * Handle file selection
     * @param {File} file - The selected file
     */
    handleFileSelection(file) {
        // Check file type
        const validTypes = ['.csv', '.xlsx', '.xls', '.json'];
        const fileExtension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
        
        if (!validTypes.includes(fileExtension)) {
            alert('Invalid file type. Please select a CSV, Excel, or JSON file.');
            return;
        }
        
        // Get file type icon
        let fileIcon = 'fa-file';
        if (fileExtension === '.csv') fileIcon = 'fa-file-csv';
        else if (fileExtension === '.json') fileIcon = 'fa-file-code';
        else if (fileExtension === '.xlsx' || fileExtension === '.xls') fileIcon = 'fa-file-excel';
        
        // Update state and UI
        this.selectedFile = file;
        this.fileInfo.innerHTML = `
            <div class="file-info-card">
                <div class="file-info-icon">
                    <i class="fas ${fileIcon}"></i>
                </div>
                <div class="file-info-details">
                    <h4 class="file-name">${file.name}</h4>
                    <div class="file-meta">
                        <span class="file-type">${file.type || 'Unknown'}</span>
                        <span class="file-size">${formatters.fileSize(file.size)}</span>
                    </div>
                </div>
            </div>
        `;
        
        // Enable buttons
        this.previewBtn.disabled = false;
        this.processBtn.disabled = false;
    }
    
    /**
     * Upload the selected file for ETL processing
     */
    async uploadFile() {
        try {
            // Show job status modal
            window.jobStatus.showModal();
            
            // Upload file
            const response = await apiService.uploadFile(this.selectedFile);
            
            // Update job status
            window.jobStatus.updateJobId(response.jobId);
            window.jobStatus.startStatusPolling(response.jobId);
            
            console.log('File uploaded successfully:', response);
        } catch (error) {
            console.error('Error uploading file:', error);
            alert('Error uploading file: ' + error.message);
            window.jobStatus.hideModal();
        }
    }
    
    /**
     * Reset the file upload state
     */
    reset() {
        this.selectedFile = null;
        this.fileInput.value = '';
        this.fileInfo.innerHTML = '<p>No file selected</p>';
        this.previewBtn.disabled = true;
        this.processBtn.disabled = true;
    }
}

// Initialize file upload when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.fileUpload = new FileUploadManager();
});
