export class GetEventRegistrationResponse {
    id: number;
    eventId: number;
    name: string;
    email: string;
    isGuest: boolean;
    registeredAt: string;

    constructor(id: number, eventId: number, name: string, email: string, isGuest: boolean, registeredAt: string) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.email = email;
        this.isGuest = isGuest;
        this.registeredAt = registeredAt;
    }
}

export default GetEventRegistrationResponse;