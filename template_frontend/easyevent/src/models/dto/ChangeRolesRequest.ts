class ChangeRolesRequest {
    roles: string[];

    constructor(roles: string[]) {
        this.roles = roles;
    }
}

export default ChangeRolesRequest;