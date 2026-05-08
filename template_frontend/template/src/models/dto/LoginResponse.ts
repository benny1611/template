export class LoginResponse {
    token: string;

    constructor(token: string) {
        this.token = token;
    }
}

export default LoginResponse;