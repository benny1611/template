export class ApiError {
    status: number;
    error: string;
    message: string;
    path: string;
    timestamp: string;

    constructor(status: number, error: string, message: string, path: string, timestamp: string) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
    }
}

export default ApiError;