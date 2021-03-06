package com.ibm.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;

// Token
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.RefreshToken;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/")
public class ProductCategoryResource {

    private static final Logger LOGGER = Logger.getLogger(ProductCategoryResource.class.getName());

    @Inject
    @IdToken
    JsonWebToken idToken;
    @Inject
    JsonWebToken accessToken;
    @Inject
    RefreshToken refreshToken;

    @Inject
    EntityManager entityManager;

    @GET
    @Path("productcategory")
    public ProductCategory[] getDefault() {
        tenantJSONWebToken("productcategory");
        return get();
    }

    @GET
    @Path("{tenant}/productcategory")
    public ProductCategory[] getTenant() {
        tenantJSONWebToken("{tenant}/productcategory");
        return get();
    }

    private ProductCategory[] get() {
        tenantJSONWebToken("private ProductCategory[] get()");
        return entityManager.createNamedQuery("ProductCategory.findAll", ProductCategory.class)
                .getResultList().toArray(new ProductCategory[0]);
    }

    @GET
    @Path("productcategory/{id}")
    public ProductCategory getSingleDefault(@PathParam("id") Integer id) {
         tenantJSONWebToken("productcategory/{id}");
         return findById(id);
    }

    @GET
    @Path("{tenant}/productcategory/{id}")
    public ProductCategory getSingleTenant(@PathParam("id") Integer id) {
        tenantJSONWebToken("{tenant}/productcategory/{id}");
        return findById(id);
    }

    private ProductCategory findById(Integer id) {
        tenantJSONWebToken("private ProductCategory findById(Integer id)");
        ProductCategory entity = entityManager.find(ProductCategory.class, id);
        if (entity == null) {
            throw new WebApplicationException("ProductCategory with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @POST
    @Transactional
    @Path("productcategory")
    public Response createDefault(ProductCategory productcategory) {
        tenantJSONWebToken("productcategory");
        return create(productcategory);
    }

    @POST
    @Transactional
    @Path("{tenant}/productcategory")
    public Response createTenant(ProductCategory productcategory) {
        tenantJSONWebToken("{tenant}/productcategory");
        return create(productcategory);
    }

    private Response create(ProductCategory productcategory) {
        if (productcategory.getId() != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }
        LOGGER.debugv("Create {0}", productcategory.getId());
        entityManager.persist(productcategory);
        return Response.ok(productcategory).status(201).build();
    }

    @PUT
    @Path("productcategory/{id}")
    @Transactional
    public ProductCategory updateDefault(@PathParam("id") Integer id, ProductCategory productcategory) {
        return update(id, productcategory);
    }

    @PUT
    @Path("{tenant}/productcategory/{id}")
    @Transactional
    public ProductCategory updateTenant(@PathParam("id") Integer id, ProductCategory productcategory) {
        return update(id, productcategory);
    }

    public ProductCategory update(@PathParam Integer id, ProductCategory productcategory) {
        if (productcategory.getId() == null) {
            throw new WebApplicationException("ProductCategory Name was not set on request.", 422);
        }

        ProductCategory entity = entityManager.find(ProductCategory.class, id);
        if (entity == null) {
            throw new WebApplicationException("ProductCategory with id of " + id + " does not exist.", 404);
        }
        entity.setId(productcategory.getId());

        LOGGER.debugv("Update #{0} ", productcategory.getId());

        return entity;
    }

    @DELETE
    @Path("productcategory/{id}")
    @Transactional
    public Response deleteDefault(@PathParam("id") Integer id) {
        return delete(id);
    }

    @DELETE
    @Path("{tenant}/productcategory/{id}")
    @Transactional
    public Response deleteTenant(@PathParam("id") Integer id) {
        return delete(id);
    }

    public Response delete(Integer id) {
        ProductCategory productcategory = entityManager.getReference(ProductCategory.class, id);
        if (productcategory == null) {
            throw new WebApplicationException("ProductCategory with id of " + id + " does not exist.", 404);
        }
        LOGGER.debugv("Delete #{0}", productcategory.getId());
        entityManager.remove(productcategory);
        return Response.status(204).build();
    }

    @GET
    @Path("productcategoryFindBy")
    public Response findByDefault(@QueryParam("type") String type, @QueryParam("value") String value) {
        return findBy(type, value);
    }

    @GET
    @Path("{tenant}/productcategoryFindBy")
    public Response findByTenant(@QueryParam("type") String type, @QueryParam("value") String value) {
        return findBy(type, value);
    }

    private Response findBy(String type, String value) {
        if (!"name".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Currently only 'productcategoryFindBy?type=name' is supported");
        }
        List<ProductCategory> list = entityManager.createNamedQuery("ProductCategory.findByName", ProductCategory.class).setParameter("name", value).getResultList();
        if (list.size() == 0) {
            return Response.status(404).build();
        }
        ProductCategory productcategory = list.get(0);
        return Response.status(200).entity(productcategory).build();
    }

    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Inject
        ObjectMapper objectMapper;

        @Override
        public Response toResponse(Exception exception) {
            LOGGER.error("Failed to handle request", exception);

            int code = 500;
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            }

            ObjectNode exceptionJson = objectMapper.createObjectNode();
            exceptionJson.put("exceptionType", exception.getClass().getName());
            exceptionJson.put("code", code);

            if (exception.getMessage() != null) {
                exceptionJson.put("error", exception.getMessage());
            }

            return Response.status(code)
                    .entity(exceptionJson)
                    .build();
        }

    }

    private String tenantJSONWebToken(String info){  
        //System.out.println("-->log: info" + info);
        try {
            Object issuer = this.accessToken.getClaim("iss");
            //System.out.println("-->log: com.ibm.catalog.ProductCategoryResource.tenantJSONWebToken issuer: " + issuer.toString());
            Object scope = this.accessToken.getClaim("scope");
            //System.out.println("-->log: com.ibm.catalog.ProductCategoryResource.tenantJSONWebToken scope: " + scope.toString());
            //System.out.println("-->log: com.ibm.catalog.ProductCategoryResource.tenantJSONWebToken access token: " + this.accessToken.toString());

            String[] parts = issuer.toString().split("/");
            //System.out.println("-->log: com.ibm.catalog.ProductCategoryResource.log part[5]: " + parts[5]);

            if (parts.length == 0) {
                return "empty";
            }
    
            return  parts[5];

        } catch ( Exception e ) {
            //System.out.println("-->log: com.ibm.catalog.ProductCategoryResource.log Exception: " + e.toString());
            return "error";
        }
    }

}
