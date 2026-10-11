package group.aelysium.rustyconnector.plugin.velocity.event_handlers.rc;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.events.EventListener;
import group.aelysium.rustyconnector.proxy.events.FamilyRegisterEvent;
import group.aelysium.rustyconnector.proxy.events.FamilyUnregisterEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

public class OnFamilyLifecycle {
    private final InetSocketAddress dummyAddress = AddressUtil.parseAddress("127.0.0.1:55555");
    protected final ProxyServer proxyServer;
    public OnFamilyLifecycle(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @EventListener
    public void handle(FamilyRegisterEvent event) {
        AtomicReference<ServerInfo> info = new AtomicReference<>(null);

        info.set(new ServerInfo(event.family.id(), dummyAddress));
        this.proxyServer.registerServer(info.get());
        event.family.storeMetadata("velocity_FamilyProxy", info.get());

        Flux<Family> flux = RC.P.Families().find(event.family.id());
        if(flux == null) return;

        flux.onStart(family -> {
            info.set(new ServerInfo(family.id(), dummyAddress));
            this.proxyServer.registerServer(info.get());
            family.storeMetadata("velocity_FamilyProxy", info.get());
        });
        flux.onClose(()->{
            if(info.get() == null) return;
            try {
                this.proxyServer.unregisterServer(info.get());
            } catch (Exception ignore) {}
        });
    }

    @EventListener
    public void handle(FamilyUnregisterEvent event) {
        try {
            this.proxyServer.unregisterServer(new ServerInfo(event.family.id(), dummyAddress));
        } catch (Exception ignore) {}
    }
}
