package org.hibernate.action.internal;

public interface ComparableEntityAction extends Comparable<ComparableEntityAction> {
	String getEntityName();

	Object getId();
}
