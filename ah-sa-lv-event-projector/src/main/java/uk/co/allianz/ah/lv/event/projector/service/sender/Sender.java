package uk.co.allianz.ah.lv.event.projector.service.sender;

public interface Sender<K> {

    void send(K message);
}
