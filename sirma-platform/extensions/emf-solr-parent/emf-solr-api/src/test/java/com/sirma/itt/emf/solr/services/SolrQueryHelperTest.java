package com.sirma.itt.emf.solr.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.testng.Assert;

import com.sirma.itt.seip.domain.instance.DefaultProperties;
import com.sirma.itt.seip.time.DateRange;
import com.sirma.itt.seip.time.ISO8601DateFormat;

/**
 * Test the solr query helper.
 *
 * @author nvelkov
 */
@SuppressWarnings("static-method")
public class SolrQueryHelperTest {

	@Test
	public void testUriQueryBuilding() {
		List<String> uris = new ArrayList<>();
		// Adding 1005 uris to test the sub query creating
		for (int i = 0; i < 1005; i++) {
			uris.add(DefaultProperties.URI + i);
		}
		String uriQuery = SolrQueryHelper.createUriQuery(uris, DefaultProperties.URI).toString();
		Assert.assertTrue(uriQuery.contains("{!df=id q.op=OR}"));

		String subQueries = uriQuery.substring(uriQuery.indexOf("(") + 1, uriQuery.lastIndexOf(")"));
		String firstSubQuery = subQueries.substring(subQueries.indexOf("(") + 1, subQueries.indexOf(")"));
		String secondSubQuery = subQueries.substring(subQueries.lastIndexOf("(") + 1, subQueries.lastIndexOf(")"));

		for (int i = 0; i < 1000; i++) {
			assertTrue(firstSubQuery.contains(DefaultProperties.URI + i));
		}
		for (int i = 1001; i < 1005; i++) {
			assertTrue(secondSubQuery.contains(DefaultProperties.URI + i));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void buildDateRangeFilterQuery_emptyFieldName() {
		SolrQueryHelper.buildDateRangeFilterQuery("", new DateRange(null, null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void buildDateRangeFilterQuery_nullFieldName() {
		SolrQueryHelper.buildDateRangeFilterQuery("", new DateRange(null, null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void buildDateRangeFilterQuery_nullDateRange() {
		SolrQueryHelper.buildDateRangeFilterQuery("fieldName", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void buildDateRangeFilterQuery_invalidDateRange() {
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, -1);
		SolrQueryHelper.buildDateRangeFilterQuery("fieldName", new DateRange(date, calendar.getTime()));
	}

	@Test
	public void buildDateRangeFilterQuery_firstDateNull() {
		StringBuilder result = SolrQueryHelper.buildDateRangeFilterQuery("fieldName",
				new DateRange(null, ISO8601DateFormat.parse("2017-02-10T20:25:30Z")));
		assertEquals("fieldName:[* TO 2017-02-10T20:25:30Z]", result.toString());
	}

	@Test
	public void buildDateRangeFilterQuery_secondDateNull() {
		StringBuilder result = SolrQueryHelper.buildDateRangeFilterQuery("fieldName",
				new DateRange(ISO8601DateFormat.parse("2017-02-10T20:25:30Z"), null));
		assertEquals("fieldName:[2017-02-10T20:25:30Z TO *]", result.toString());
	}

	@Test
	public void buildDateRangeFilterQuery_bothDatesNull() {
		StringBuilder result = SolrQueryHelper.buildDateRangeFilterQuery("fieldName", new DateRange(null, null));
		assertEquals("fieldName:[* TO *]", result.toString());
	}

}
