export class UserDTO {
    id: number;
    email: string;
    name: string;
    profilePicture: string|null;
    language: string|null;
    oldPassword: string|null;
    newPassword: string|null;
    token: string|null;
    isLocalPasswordSet: boolean;

    constructor(email: string, name: string, profilePicture: string|null, language: string|null, oldPassword: string|null, newPassword: string|null, token: string|null, id: number, isLocalPasswordSet: boolean) {
        this.email = email;
        this.name = name;
        this.profilePicture = profilePicture;
        this.language = language;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.token = token;
        this.id = id;
        this.isLocalPasswordSet = isLocalPasswordSet;
    }
}

export default UserDTO;