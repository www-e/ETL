/**
 * API Service for ETL operations
 * Handles all communication with the backend REST API
 */
class ApiService {
    constructor() {
        this.baseUrl = '/api/etl';
    }

    /**
     * Upload a file for ETL processing
     * @param {File} file - The file to upload
     * @returns {Promise} - Promise with the response data
     */
    async uploadFile(file) {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch(`${this.baseUrl}/upload`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error uploading file:', error);
            throw error;
        }
    }

    /**
     * Preview a file's contents
     * @param {File} file - The file to preview
     * @returns {Promise} - Promise with the preview data
     */
    async previewFile(file) {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch(`${this.baseUrl}/preview`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error previewing file:', error);
            throw error;
        }
    }

    /**
     * Get the status of an ETL job
     * @param {string} jobId - The job ID
     * @returns {Promise} - Promise with the job status
     */
    async getJobStatus(jobId) {
        try {
            const response = await fetch(`${this.baseUrl}/status/${jobId}`);

            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error getting job status:', error);
            throw error;
        }
    }

    /**
     * Get all processed data
     * @returns {Promise} - Promise with the processed data
     */
    async getAllData() {
        try {
            const response = await fetch(`${this.baseUrl}/data`);

            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error getting processed data:', error);
            throw error;
        }
    }

    /**
     * Get ETL statistics
     * @returns {Promise} - Promise with the statistics
     */
    async getStatistics() {
        try {
            const response = await fetch(`${this.baseUrl}/stats`);

            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error getting statistics:', error);
            throw error;
        }
    }
}

// Create a singleton instance
const apiService = new ApiService();
