import * as React from "react";
import * as ReactDOM from "react-dom";

type Props = {}

type State = {}

class ChatRoom extends React.PureComponent<Props, State> {

    constructor(props: Props) {
        super(props);
        this.state = {};
    }

    render() {

        return <div className="chat">

        </div>;

    }

}

ReactDOM.render(<ChatRoom/>, document.getElementById("main"));