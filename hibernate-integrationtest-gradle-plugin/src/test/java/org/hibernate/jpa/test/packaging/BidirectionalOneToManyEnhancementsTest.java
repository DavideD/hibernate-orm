/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.packaging;

import org.hibernate.orm.integrationtest.bytecode.model.Book;
import org.hibernate.orm.integrationtest.bytecode.model.Person;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks the association management enhancement is working
 * when using the hibernate gradle plugin.
 */
public class BidirectionalOneToManyEnhancementsTest {

	final Book book = new Book( 5L, "Lost Connections: Why You’re Depressed and How to Find Hope", " 978-1408878729" );
	final Person author = new Person( 5L, "Johann Hari" );

	@Test
	public void normalBehaviourWithoutEnhancements() throws Exception {
		// This test should pass with or without enhancements
		book.setAuthor( author );
		author.getBooks().add( book );

		assertEquals( 1, author.getBooks().size() );
		assertEquals( book, author.getBooks().get( 0 ) );
		assertEquals( author, book.getAuthor() );
	}

	@Test
	public void addToField() throws Exception {
		book.setAuthor( author );
		// Normally, without enhancements, you would also need:
		// author.getBooks().add( book );

		assertEquals( 1, author.getBooks().size() );
		assertEquals( book, author.getBooks().get( 0 ) );
	}

	@Test
	public void addToCollection() throws Exception {
		author.getBooks().add( book );
		// Normally, without enhancements, you would also need:
		// book.setAuthor( author );

		assertEquals( author, book.getAuthor() );
	}

	@Test
	public void setFieldToNull() throws Exception {
		book.setAuthor( author );
		author.getBooks().add( book );

		book.setAuthor( null );
		// Normally, without enhancements, you would also need:
		// author.getBooks().remove( book );

		assertTrue( author.getBooks().isEmpty() );
	}
}
