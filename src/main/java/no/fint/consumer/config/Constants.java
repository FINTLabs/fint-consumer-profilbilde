package no.fint.consumer.config;

public enum Constants {
    ;

    public static final String COMPONENT = "profilbilde";
    public static final String COMPONENT_CONSUMER = COMPONENT + " consumer";
    public static final String CACHE_SERVICE = "CACHE_SERVICE";
    public static final String CACHE_INITIALDELAY_PROFILBILDE = "${fint.consumer.cache.initialDelay.profilbilder:900000}";

    public static final String CACHE_FIXEDRATE_PROFILBILDE = "${fint.consumer.cache.fixedRate.profilbilder:900000}";
}
