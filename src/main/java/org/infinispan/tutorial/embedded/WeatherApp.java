package org.infinispan.tutorial.embedded;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import java.util.concurrent.TimeUnit;

public class WeatherApp {

    static final String[] locations = {
            "Taipei, TW",
            "Kaohsiung, TW",
            "Tokyo, JP",
            "Asahikawa, JP"
    };
//    static final String[] locations = {"Taipei, TW", "Kaohsiung, TW", "Rome, Italy", "Como, Italy", "Basel, Switzerland", "Bern, Switzerland",
//                                       "London, UK", "Newcastle, UK", "Bucarest, Romania", "Cluj-Napoca, Romania", "Ottawa, Canada",
//                                       "Toronto, Canada", "Lisbon, Portugal", "Porto, Portugal", "Raleigh, USA", "Washington, USA"};

    private final EmbeddedCacheManager cacheManager;

    private Cache<String, LocationWeather> cache;

    private final WeatherService weatherService;

    private final ClusterListener clusterListener;

    public WeatherApp() throws InterruptedException {
        // global - cluster configuration

        GlobalConfigurationBuilder configurationBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
        configurationBuilder.transport().clusterName("WeatherApp");

        cacheManager = new DefaultCacheManager(configurationBuilder.build());

        // listener

        clusterListener = new ClusterListener(2);

        cacheManager.addListener(clusterListener);

        // config

        ConfigurationBuilder config = new ConfigurationBuilder();
        
        config.expiration().lifespan(5, TimeUnit.SECONDS)
              .clustering().cacheMode(CacheMode.DIST_SYNC);

        cacheManager.defineConfiguration("weather", config.build());

        cache = cacheManager.getCache("weather");

        cache.addListener(new CacheListener());

        weatherService = initWeatherService();

        System.out.println("---- Waiting for cluster to complet initialization ----");

        clusterListener.clusterFormedLatch.await();

        System.out.println("---- Initialized completed ----");
    }

    private WeatherService initWeatherService() {
        String apiKey = System.getenv("OWMAPIKEY");
        
        if (apiKey == null) {
            System.out.println("WARNING: OWMAPIKEY environment variable not set, using the RandomWeatherService.");
            return new RandomWeatherService(cache);
        } else {
            return new OpenWeatherMapService(apiKey, cache);
        }
    }

    public void fetchWeather() {
        System.out.println("---- Fetching weather information ----");
        long start = System.currentTimeMillis();
        for (String location : locations) {
            LocationWeather weather = weatherService.getWeatherForLocation(location);
            System.out.printf("%s - %s\n", location, weather);
        }
        System.out.printf("---- Fetched in %dms ----\n", System.currentTimeMillis() - start);
    }

    public void shutdown() throws InterruptedException {
        // If the local node is coordinator node, shutdown directly without waiting,
        // so the one of the other non-coordinator nodes will become the coordinator node?
        if (!cacheManager.isCoordinator()) {
            System.out.println("Non-coordinator node waits for shutdown.");

            clusterListener.shutdownLatch.await();

            System.out.println("Non-coordinator node ready to shutdown.");
        } else {
            System.out.println("Coordinator node shutdown directly without waiting.");
        }

        cacheManager.stop();
    }

    public static void main(String[] args) throws Exception {
        WeatherApp app = new WeatherApp();

        if (app.cacheManager.isCoordinator()) {
            app.fetchWeather();

            app.fetchWeather();

            TimeUnit.SECONDS.sleep(5);

            app.fetchWeather();
        }

        app.shutdown();
    }

}
