package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import org.bson.types.ObjectId;

public class Invoice_AddressCriteria {
  private Query<com.antwerkz.critter.Invoice.Address> query;
  private String prefix;

  public Invoice_AddressCriteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix;
  }


  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice.Address, java.lang.String> city() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".city"));
  }

  public Invoice_AddressCriteria city(java.lang.String value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".city")).equal(value);
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice.Address, java.lang.String> state() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".state"));
  }

  public Invoice_AddressCriteria state(java.lang.String value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".state")).equal(value);
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice.Address, java.lang.String> zip() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".zip"));
  }

  public Invoice_AddressCriteria zip(java.lang.String value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".zip")).equal(value);
    return this;
  }
}
