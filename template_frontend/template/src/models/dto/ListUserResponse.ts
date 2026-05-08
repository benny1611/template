export class ListUserResponse {
    id: number;
    name: string;
    email: string;
    profilePicture: string | null;
    active: boolean;
    banned: boolean;
    roles: string[];
    softDeleted: boolean;
    deletedAt: string | null;

    constructor(id: number, name: string, email: string, profilePicture: string|null, active: boolean, banned: boolean, roles: string[], softDeleted: boolean, deletedAt: string|null) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profilePicture = profilePicture;
        this.active = active;
        this.banned = banned;
        this.roles = roles;
        this.softDeleted = softDeleted;
        this.deletedAt = deletedAt;
    }
}

export default ListUserResponse;