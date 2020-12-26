import * as React from "react";
import * as ReactDOM from "react-dom";
import {io, Socket} from 'socket.io-client';

type Props = {}

type State = {
    connected?: boolean;
    messages: []
    message?: string;
}

class ChatRoom extends React.PureComponent<Props, State> {

    private socket: Socket;

    constructor(props: Props) {
        super(props);
        this.state = {messages: []};
    }

    render() {

        return <div className="chat">

            {!this.state.connected && <div>Connecting ...</div>}

            <ul className="messages">
                {this.state.messages.map(m => <li>{m}</li>)}
            </ul>

            <form onSubmit={e => {
                e.preventDefault();
                this.sendChat();
            }}>

                <input
                    type="text"
                    value={this.state.message}
                    onChange={e => this.setState({message: e.target.value})}/>

                <input
                    disabled={!this.state.connected}
                    type="submit"
                    value="Send"/>

            </form>

        </div>;

    }

    componentDidMount() {
        const socket = this.socket = io("ws://localhost:8091");
        socket.on("connect", r => {
            console.log(r);
            this.setState({connected: true});
        });
        socket.on("disconnect", r => {
            console.error(r);
            this.setState({connected: false});
        });
        socket.open();
    }

    private sendChat() {
        const message = this.state.message;
        if (message != null) this.socket.emit("message", message);
        this.setState({message: ""});
    }

}

ReactDOM.render(<ChatRoom/>, document.getElementById("main"));