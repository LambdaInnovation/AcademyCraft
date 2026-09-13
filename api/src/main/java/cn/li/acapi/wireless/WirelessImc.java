package cn.li.acapi.wireless;

/**
 * Inter-Mod Communication (IMC) constants for the wireless energy system.
 *
 * <p>External mods observe wireless topology changes by sending an IMC message
 * during {@code InterModEnqueueEvent} (Forge):
 *
 * <pre>{@code
 * InterModComms.sendTo(WirelessImc.MOD_ID, WirelessImc.REGISTER_NETWORK_HANDLER,
 *     () -> (java.util.function.Consumer<java.util.Map<?, ?>>) event -> {
 *         // inspect the event map, see key contract below
 *     });
 * }</pre>
 *
 * <p><b>Payload:</b> the message payload must be a
 * {@code java.util.function.Consumer<java.util.Map<?, ?>>} (Clojure callers may
 * pass an {@code IFn} instead). Handlers are invoked on the server thread after
 * the corresponding topology change. A handler that throws is logged and removed.
 *
 * <p><b>Event map contract</b> — keys are Clojure keywords
 * ({@code clojure.lang.Keyword.intern("action")} etc.), values as listed:
 *
 * <ul>
 *   <li>Network events ({@link #REGISTER_NETWORK_HANDLER}):
 *     <ul>
 *       <li>{@code :kind} — {@code :topology/network}</li>
 *       <li>{@code :action} — {@code :created} | {@code :destroyed}</li>
 *       <li>{@code :ssid} — {@code String}</li>
 *       <li>{@code :matrix} — {@link IWirelessMatrix}</li>
 *     </ul>
 *   </li>
 *   <li>Node events ({@link #REGISTER_NODE_HANDLER}):
 *     <ul>
 *       <li>{@code :kind} — {@code :topology/node}</li>
 *       <li>{@code :action} — {@code :connected} | {@code :disconnected} |
 *           {@code :generator-linked} | {@code :generator-unlinked} |
 *           {@code :receiver-linked} | {@code :receiver-unlinked}</li>
 *       <li>{@code :node} — {@link IWirelessNode}</li>
 *       <li>{@code :matrix} — {@link IWirelessMatrix}, present on
 *           {@code :connected} / {@code :disconnected}</li>
 *       <li>{@code :generator} — {@link IWirelessGenerator}, present on
 *           generator link events</li>
 *       <li>{@code :receiver} — {@link IWirelessReceiver}, present on
 *           receiver link events</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class WirelessImc {

    /** The mod-id of this mod; use as target id in platform messaging. */
    public static final String MOD_ID = "academycraft";

    /** IMC key: register a network topology event consumer. */
    public static final String REGISTER_NETWORK_HANDLER = "register_wireless_network_handler";

    /** IMC key: register a node topology event consumer. */
    public static final String REGISTER_NODE_HANDLER = "register_wireless_node_handler";

    private WirelessImc() {}
}
