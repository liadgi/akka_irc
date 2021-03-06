import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

import java.util.LinkedList;
import java.util.List;

public class ChannelCreator extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(JoinMessage.class, joinMessage -> {

            // create the channel
            ActorRef channelToJoin = getContext().actorOf(Props.create(ChannelActor.class, joinMessage.channelName), joinMessage.channelName);
                    //.withMailbox("akka.dispatch.UnboundedMailbox"));

            channelToJoin.forward(joinMessage, getContext());

            sendChannelListToClient();
        })

        .match(GetChannelListMessage.class, chLstMsg -> {
            sendChannelListToClient();

        })
        .match(KillChannelMessage.class, klChMsg -> {
            ActorSelection sel = getContext().actorSelection(klChMsg.channelName);
            ActorRef channelToKill = HelperFunctions.getActorRefBySelection(sel);

            channelToKill.forward(akka.actor.PoisonPill.getInstance(), getContext());
        }).build();
    }

    private void sendChannelListToClient() {
        // Preferring that ChannelCreator will send all the users the channels list,
        // instead of making all the channels send its name to every user.
        Iterable<ActorRef> children = getContext().getChildren();
        List<String> channelNames = new LinkedList<>();
        children.forEach(childChannel -> channelNames.add(childChannel.path().name()));

        SetChannelListMessage setChLstMsg = new SetChannelListMessage();
        setChLstMsg.channels = channelNames;
        sender().tell(setChLstMsg, self());
    }

    @Override
    public void preStart() {

    }
}
