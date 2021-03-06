package org.pac4j.core.profile;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an helper for profiles.
 * 
 * @author Jerome Leleu
 * @since 1.1.0
 */
public final class ProfileHelper {

    private static final Logger logger = LoggerFactory.getLogger(ProfileHelper.class);

    private static final Map<String, Constructor<? extends CommonProfile>> constructorsCache = new ConcurrentHashMap<>();

    /**
     * Indicate if the user identifier matches this kind of profile.
     * 
     * @param id user identifier
     * @param clazz profile class
     * @return if the user identifier matches this kind of profile
     */
    public static boolean isTypedIdOf(final String id, final Class<? extends CommonProfile> clazz) {
        if (id != null && clazz != null) {
            return id.startsWith(clazz.getName() + CommonProfile.SEPARATOR);
        }
        return false;
    }

    /**
     * Build a profile from a typed id and a map of attributes.
     * 
     * @param typedId typed identifier
     * @param attributes user attributes
     * @return the user profile built
     */
    public static CommonProfile buildProfile(final String typedId, final Map<String, Object> attributes) {
        if (CommonHelper.isBlank(typedId)) {
            return null;
        }

        logger.info("Building user profile based on typedId {}", typedId);
        try {
            final String[] values = typedId.split(CommonProfile.SEPARATOR);
            if (values != null && values.length >= 1) {
                final String className = values[0];
                if (CommonHelper.isNotBlank(className)) {
                    final CommonProfile profile = buildUserProfileByClassCompleteName(className);
                    profile.build(typedId, attributes);
                    logger.debug("userProfile built: {}", profile);
                    return profile;
                }
            }
        } catch (final TechnicalException e) {
            logger.error("Cannot build instance", e);
        }
        return null;
    }

    /**
     * Build a profile by its class name.
     *
     * @param completeName the class name
     * @return the built user profile
     */
    public static CommonProfile buildUserProfileByClassCompleteName(final String completeName) {
        try {
            final Constructor<? extends CommonProfile> constructor = getConstructor(completeName);
            return constructor.newInstance();
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new TechnicalException(e);
        }
    }

    /**
     * Get the constructor of the class.
     *
     * @param name the name of the class
     * @return the constructor
     * @throws ClassNotFoundException class not found
     * @throws NoSuchMethodException method not found
     */
    @SuppressWarnings("unchecked")
    private static Constructor<? extends CommonProfile> getConstructor(final String name) throws ClassNotFoundException, NoSuchMethodException {
        Constructor<? extends CommonProfile> constructor = constructorsCache.get(name);
        if (constructor == null) {
            synchronized (constructorsCache) {
                constructor = constructorsCache.get(name);
                if (constructor == null) {
                	ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                	
                	if (tccl == null) {    	
                		constructor = (Constructor<? extends CommonProfile>) Class
                            	.forName(name).getDeclaredConstructor();
                	} else {
                        constructor = (Constructor<? extends CommonProfile>) Class
                                .forName(name, true, tccl).getDeclaredConstructor();
                	}
                    constructorsCache.put(name, constructor);
                }
            }
        }
        return constructor;
    }

    /**
     * Flat the linked hashmap of profiles into a single optional profile.
     *
     * @param profiles the linked hashmap of profiles
     * @param <U> the kind of profile
     * @return the (optional) profile
     */
    public static <U extends CommonProfile> Optional<U> flatIntoOneProfile(final LinkedHashMap<String, U> profiles) {
        if (profiles.size() == 0) {
            return Optional.empty();
        } else {
            U profile = null;
            final Iterator<U> profilesList = profiles.values().iterator();
            while (profilesList.hasNext()) {
                final U nextProfile = profilesList.next();
                if (profile == null || profile instanceof AnonymousProfile) {
                    profile = nextProfile;
                }
            }
            return Optional.of(profile);
        }
    }

    /**
     * Flat the linked hashmap of profiles into a list of profiles.
     *
     * @param profiles the linked hashmap of profiles
     * @param <U> the kind of profile
     * @return the list of profiles
     */
    public static <U extends CommonProfile> List<U> flatIntoAProfileList(final LinkedHashMap<String, U> profiles) {
        final List<U> listProfiles = new ArrayList<>();
        for (final Map.Entry<String, U> entry : profiles.entrySet()) {
            listProfiles.add(entry.getValue());
        }
        return Collections.unmodifiableList(listProfiles);
    }
}
