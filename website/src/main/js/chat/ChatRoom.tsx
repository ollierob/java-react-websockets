import * as React from "react";
import * as ReactDOM from "react-dom";
import "./Chat.css";
import {ChatMessage} from "../../protobuf/chat_pb";

type Props = {}

type State = {
    connected?: boolean;
    messages: ChatMessage.AsObject[]
    message?: string;
    username?: string;
}

class ChatRoom extends React.PureComponent<Props, State> {

    private socket: WebSocket;

    constructor(props: Props) {
        super(props);
        this.state = {
            username: "default",
            messages: []
        };
    }

    render() {

        return <div className="chat">

            {!this.state.connected && <div>Connecting ...</div>}

            <ul className="messages">
                {this.state.messages.map(m => <Message key={m.id} message={m}/>)}
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
        if (this.socket != null) {
            this.socket.close(4000);
            this.socket = null;
        }
        const socket = new WebSocket("ws://localhost:8090/chat/subscribe?username=" + this.state.username);
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
            //Auto-reconnect
            if (r.code != 4000) setTimeout(() => this.openSocket(), 2000);
        };
        socket.onmessage = r => {
            const data: Blob = r.data;
            data.arrayBuffer().then(buff => {
                const array = new Uint8Array(buff);
                if (!array.byteLength) return;
                const message = ChatMessage.deserializeBinary(array).toObject();
                this.setState(current => {
                    const messages = [...current.messages];
                    messages.push(message);
                    return {messages};
                });
            });
        };
        this.socket = socket;
    }

    private sendChat() {
        const message = this.state.message;
        if (message != null) this.socket.send(message);
        this.setState({message: ""});
    }

}

const Message = (props: {message: ChatMessage.AsObject}) => {
    const message = props.message;
    return <li>
        <span className="username">{message.user}</span>
        <span className="message">{message.message}</span>
    </li>;
};

ReactDOM.render(<ChatRoom/>, document.getElementById("main"));