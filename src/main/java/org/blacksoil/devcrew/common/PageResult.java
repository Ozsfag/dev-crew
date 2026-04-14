package org.blacksoil.devcrew.common;

import java.util.List;

/**
 * Обёртка для пагинированных результатов. Используется в domain-портах вместо Spring Page, чтобы не
 * нарушать правило "domain не зависит от Spring".
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

  public int totalPages() {
    return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
  }

  public boolean hasNext() {
    return (long) (page + 1) * size < totalElements;
  }
}
