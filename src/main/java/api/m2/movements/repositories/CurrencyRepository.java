package api.m2.movements.repositories;

import api.m2.movements.configuration.CacheConfiguration;
import api.m2.movements.entities.Currency;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    @Cacheable(CacheConfiguration.CURRENCY_CACHE)
    Optional<Currency> findBySymbol(String symbol);

    @Query(value = """
    select c.id
    from Currency c
    where c.symbol in :symbols
""")
    List<Integer> findAllBySymbol(List<String> symbols);

    @Cacheable(CacheConfiguration.CURRENCY_CACHE)
    List<Currency> findAllByEnabled(boolean enabled);
}
