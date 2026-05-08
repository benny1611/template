export class CreateEventRequest {
    title: string;
    numberOfSeats: number;
    date: string;

    constructor(title: string, numberOfSeats: number, date: string) {
        this.title = title;
        this.numberOfSeats = numberOfSeats;
        this.date = date;
    }
}

export default CreateEventRequest;