package uk.co.allianz.ah.lv.event.projector.log;

import net.logstash.logback.marker.Markers;
import org.slf4j.Marker;

import static uk.co.allianz.ah.lv.event.projector.log.LogContext.ACTION_STATUS;
import static uk.co.allianz.ah.lv.event.projector.log.LogContext.ACTION_TYPE;

public final class LogType {

    private LogType() {
    }

    /* ACTION TYPES */
    public static final Marker EVENT_RECEPTION = Markers.append(ACTION_TYPE, "EVENT_RECEPTION");
    public static final Marker EVENT_PROJECTION = Markers.append(ACTION_TYPE, "EVENT_PROJECTION");
    public static final Marker ONPREM_EVENT_RECEIVER_API_CALL = Markers.append(ACTION_TYPE, "ONPREM_EVENT_RECEIVER_API_CALL");

    /* ACTION STATUSES */
    public static final Marker START = Markers.append(ACTION_STATUS, "START");
    public static final Marker IGNORED = Markers.append(ACTION_STATUS, "IGNORED");
    public static final Marker SUCCESS = Markers.append(ACTION_STATUS, "SUCCESS");
    public static final Marker ERROR = Markers.append(ACTION_STATUS, "ERROR");
}
