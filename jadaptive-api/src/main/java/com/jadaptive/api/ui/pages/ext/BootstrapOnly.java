package com.jadaptive.api.ui.pages.ext;

import static com.jadaptive.api.ui.PageHelper.appendHeadScript;
import static com.jadaptive.api.ui.PageHelper.appendStylesheet;
import static com.jadaptive.utils.FileUtils.checkEndsWithSlash;
import static com.jadaptive.utils.FileUtils.checkStartsWithNoSlash;
import static com.jadaptive.utils.Npm.scripts;

import java.nio.file.Files;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jadaptive.api.app.ConfigLocations;
import com.jadaptive.api.db.ClassLoaderService;
import com.jadaptive.api.ui.AbstractPageExtension;
import com.jadaptive.api.ui.Page;

@Component
public class BootstrapOnly extends AbstractPageExtension {

	static Logger log = LoggerFactory.getLogger(BootstrapOnly.class);
	
	@Autowired
	private ClassLoaderService classService; 
	
	private String runtimePathJs = null;
	private String runtimePathCss = null;
	
	@Override
	public void process(Document document, Element element, Page page) {
	
		if(Objects.isNull(runtimePathJs)) {
			
			runtimePathJs = "/app/content/npm2mvn/npm/bootstrap/current/dist/js/bootstrap.bundle.min.js";
			runtimePathCss = "/app/content/npm2mvn/npm/bootstrap/current/dist/css/bootstrap.min.css";
			
			Collection<Class<?>> classes = classService.resolveAnnotatedClasses(EnableBootstrapTheme.class);
			if(!classes.isEmpty()) {
				Class<?> clz = classes.iterator().next();
				EnableBootstrapTheme a = clz.getAnnotation(EnableBootstrapTheme.class);
				if(StringUtils.isNotBlank(a.path()) && page.isThemePage()) {
					
					if(Files.exists(ConfigLocations.Defaults.get().getDropInConfig().resolve("system", "shared", "webapp", a.path()))) {
						runtimePathJs = "/app/content/npm2mvn/npm/" + checkStartsWithNoSlash(checkEndsWithSlash(a.path())) + "dist/js/bootstrap.bundle.min.js";
						runtimePathCss = "/app/content/npm2mvn/npm/" + checkStartsWithNoSlash(checkEndsWithSlash(a.path())) + "dist/css/bootstrap.min.css"; 
					}
				}
			}
		}
		
		scripts(document, "@popperjs/core", "dist/umd/popper.min.js");
		appendHeadScript(document, runtimePathJs, false, "bootstrapEnabled");
		appendStylesheet(document, runtimePathCss, "bootstrapCss");
		appendStylesheet(document, runtimePathCss, "printBootstrap", "print");

	}

	@Override
	public String getName() {
		return "bootstrap-only";
	}

}
