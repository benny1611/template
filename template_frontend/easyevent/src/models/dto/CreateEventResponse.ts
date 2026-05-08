export class CreateEventResponse {
    id: number;
    title: string;
    date: string;
    numberOfSeats: number;

    constructor(id: number, title: string, date: string, numberOfSeats: number) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.numberOfSeats = numberOfSeats;
    }
}

export default CreateEventResponse;