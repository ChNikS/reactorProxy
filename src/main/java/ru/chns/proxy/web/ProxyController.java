package ru.chns.proxy.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.chns.proxy.service.CacheProxyService;

@Controller
public class ProxyController {
   @Autowired
   private CacheProxyService service;

   private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);

   @RequestMapping(value = "/{x}/{y}/{z}", method=RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
   @ResponseBody
   public Mono<byte[]> getImage (@PathVariable("x") String x, @PathVariable("y") String y, @PathVariable("z") String z) {
       logger.debug("have request: "+x+"; "+y+"; "+z);
       return service.getFile("/"+x+"/"+y+"/"+z);
    }

}

