export class PasswordResetConfirmRequest {
    secret: string;
    tokenId: string;
    newPassword: string;

    constructor(secret: string, tokenId: string, newPassword: string) {
        this.secret = secret;
        this.tokenId = tokenId;
        this.newPassword = newPassword;
    }
}

export default PasswordResetConfirmRequest;