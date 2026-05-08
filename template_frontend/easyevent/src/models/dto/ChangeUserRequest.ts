export class ChangeUserRequest {
    email: string|null;
    name: string|null;

    constructor(email: string|null, name: string|null) {
        this.email = email;
        this.name = name;
    }
}

export default ChangeUserRequest;