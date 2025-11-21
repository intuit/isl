---
layout: none
---
/**
 * Lunr lunr-en.js
 */
var idx = lunr(function () {
  this.field('title', { boost: 10 });
  this.field('excerpt');
  this.field('categories');
  this.field('tags');
  this.ref('id');

  for (var item in store) {
    this.add({
      title: store[item].title,
      excerpt: store[item].excerpt,
      categories: store[item].categories,
      tags: store[item].tags,
      id: item
    })
  }
});

console.log( jQuery.type(idx) );

