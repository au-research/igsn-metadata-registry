package au.edu.ardc.registry.common.controller.api.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/admin/caches")
public class EvictCacheController {

	@Autowired
	CacheManager cacheManager;

	@GetMapping("")
	public ResponseEntity<?> index() {
		return ResponseEntity.ok().body(cacheManager.getCacheNames());
	}

	@DeleteMapping("")
	public ResponseEntity<?> destroy() {
		for (String cacheName : cacheManager.getCacheNames()) {
			Cache cache = cacheManager.getCache(cacheName);
			if (cache != null) {
				cache.clear();
			}
		}
		return ResponseEntity.accepted().body("Caches have been evicted");
	}

}
