export class PasswordResetRequest {
    email: string;

    constructor(email: string) {
        this.email = email;
    }
}

export default PasswordResetRequest;