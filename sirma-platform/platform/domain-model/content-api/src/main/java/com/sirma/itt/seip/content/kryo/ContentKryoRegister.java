package com.sirma.itt.seip.content.kryo;

import java.util.Arrays;
import java.util.List;

import com.sirma.itt.seip.Pair;
import com.sirma.itt.seip.content.StoreItemInfo;
import com.sirma.itt.seip.plugin.Extension;
import com.sirma.itt.seip.serialization.kryo.KryoInitializer;

/**
 * Extension to register the serializable class from content-api to Kryo.
 * 
 * @author BBonev
 */
@Extension(target = KryoInitializer.TARGET_NAME, order = 150)
public class ContentKryoRegister implements KryoInitializer {

	@Override
	public List<Pair<Class<?>, Integer>> getClassesToRegister() {
		return Arrays.asList(new Pair<>(StoreItemInfo.class, 443));
	}

}
