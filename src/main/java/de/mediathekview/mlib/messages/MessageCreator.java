package de.mediathekview.mlib.messages;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import de.mediathekview.mlib.messages.listener.MessageListener;

public abstract class MessageCreator {
  protected final Collection<MessageListener> messageListeners;

  public MessageCreator() {
    super();
    messageListeners = ConcurrentHashMap.newKeySet();
  }

  public MessageCreator(final MessageListener... aListeners) {
    this();
    messageListeners.addAll(Arrays.asList(aListeners));
  }

  public boolean addAllMessageListener(final Collection<MessageListener> aMessageListeners) {
    return messageListeners.addAll(aMessageListeners);
  }

  public boolean addMessageListener(final MessageListener aMessageListener) {
    return messageListeners.add(aMessageListener);
  }

  public boolean removeAllMessageListener(final Collection<MessageListener> aMessageListeners) {
    return messageListeners.removeAll(aMessageListeners);
  }

  public boolean removeMessageListener(final MessageListener aMessageListener) {
    return messageListeners.remove(aMessageListener);
  }

  protected void publishMessage(final Message aMessage, final Object... aParams) {
    messageListeners.parallelStream().forEach(l -> l.consumeMessage(aMessage, aParams));
  }

}