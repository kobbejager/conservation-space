package com.sirma.itt.seip.instance.archive;

import static com.sirma.itt.seip.domain.instance.DefaultProperties.VERSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sirma.itt.seip.convert.Converter;
import com.sirma.itt.seip.convert.TypeConverter;
import com.sirma.itt.seip.convert.TypeConverterImpl;
import com.sirma.itt.seip.domain.instance.ArchivedInstance;
import com.sirma.itt.seip.domain.instance.EmfInstance;
import com.sirma.itt.seip.domain.instance.Instance;
import com.sirma.itt.seip.mapping.ObjectMapper;

/**
 * Test for {@link InstanceToArchivedInstanceConverterProvider}.
 *
 * @author A. Kunchev
 */
public class InstanceToArchivedInstanceConverterProviderTest {

	@InjectMocks
	private InstanceToArchivedInstanceConverterProvider provider;

	@Mock
	private ObjectMapper dozerMapper;

	@Mock
	private TransactionIdHolder transactionIdHolder;

	@Mock
	private TypeConverter typeConverter;

	@Before
	public void setup() {
		provider = new InstanceToArchivedInstanceConverterProvider();
		MockitoAnnotations.initMocks(this);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void register() {
		provider.register(typeConverter);
		verify(typeConverter).addConverter(eq(Instance.class), eq(ArchivedInstance.class), any(Converter.class));
	}

	@Test
	public void convertTest() {
		TypeConverter converter = new TypeConverterImpl();
		provider.register(converter);

		EmfInstance instance = new EmfInstance();
		instance.setId("instance-id");
		instance.add(VERSION, "1.7");
		when(dozerMapper.map(instance, ArchivedInstance.class)).thenReturn(new ArchivedInstance());
		when(transactionIdHolder.getTransactionId()).thenReturn("transaction-id");

		ArchivedInstance converted = converter.convert(ArchivedInstance.class, instance);

		assertEquals("instance-id-v1.7", converted.getId());
		assertEquals("transaction-id", converted.getTransactionId());
	}

}
