import * as React from "react";
import * as ReactDOM from "react-dom";

type Props = {}

type State = {
    connected?: boolean;
    messages: string[]
    message?: string;
}

class ChatRoom extends React.PureComponent<Props, State> {

    private socket: WebSocket;

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
        this.openSocket();
    }

    private openSocket() {
        const socket = this.socket = new WebSocket("ws://localhost:8090/chat/subscribe");
        socket.onopen = r => {
            console.log(r);
            this.setState({connected: true});
        };
        socket.onerror = r => {
            console.error(r);
            this.setState({connected: false});
        };
        socket.onclose = r => {
            console.error(r);
            this.setState({connected: false});
        };
        socket.onmessage = r => {
            const message: string = r.data;
            this.setState(current => {
                const messages = [...current.messages];
                messages.push(message);
                return {messages};
            });
        };
    }

    private sendChat() {
        const message = this.state.message;
        if (message != null) this.socket.send(message);
        this.setState({message: ""});
    }

}

ReactDOM.render(<ChatRoom/>, document.getElementById("main"));