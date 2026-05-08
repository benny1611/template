export class EventRegistrationRequest {
    name: string;
    email: string;
    eventId: number;

    constructor(name: string, email: string, eventId: number) {
        this.name = name;
        this.email = email;
        this.eventId = eventId;
    }
}

export default EventRegistrationRequest;