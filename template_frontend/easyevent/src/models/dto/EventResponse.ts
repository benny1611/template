export class EventResponse {
    id: number;
    title: string;
    numberOfSeats: number;
    date: string;
    createdByEmail: string;

    constructor(id: number, title: string, numberOfSeats: number, date: string, createdByEmail: string) {
        this.id = id;
        this.title = title;
        this.numberOfSeats = numberOfSeats;
        this.date = date;
        this.createdByEmail = createdByEmail;
    }
}

export default EventResponse;