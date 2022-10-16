package com.waracle.cakemgr;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = {"/cakes"})
public class CakeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public void init() throws ServletException {
        super.init();

        System.out.println("init started");
        Set<String> titleSet = new HashSet<>();

        System.out.println("downloading cake json");
        try (InputStream inputStream = new URL("https://gist.githubusercontent.com/hart88/198f29ec5114a3ec3460/raw/8dd19a88f9b8d24c23d9960f3300d0c917a4f07c/cake.json").openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuffer buffer = new StringBuffer();
            String line = reader.readLine();
            while (line != null) {
                buffer.append(line);
                line = reader.readLine();
            }

            System.out.println("parsing cake json");
            JsonParser parser = new JsonFactory().createParser(buffer.toString());
            if (JsonToken.START_ARRAY != parser.nextToken()) {
                throw new Exception("bad token");
            }

            JsonToken nextToken = parser.nextToken();
            while(nextToken == JsonToken.START_OBJECT) {
                System.out.println("creating cake entity");

                System.out.println(parser.nextFieldName());
                String title = parser.nextTextValue();
                boolean isAdded = titleSet.add(title);
                CakeEntity cakeEntity = new CakeEntity();
                cakeEntity.setTitle(title);

                System.out.println(parser.nextFieldName());
                cakeEntity.setDesc(parser.nextTextValue());

                System.out.println(parser.nextFieldName());
                cakeEntity.setImage(parser.nextTextValue());

                if(isAdded) {
                    Session session = HibernateUtil.getSessionFactory().openSession();
                    try {
                        session.beginTransaction();
                        session.persist(cakeEntity);
                        System.out.println("adding cake entity");
                        session.getTransaction().commit();
                    } catch (ConstraintViolationException ex) {

                    }
                    session.close();
                }
                nextToken = parser.nextToken();
                System.out.println(nextToken);

                nextToken = parser.nextToken();
                System.out.println(nextToken);
            }

        } catch (Exception ex) {
            throw new ServletException(ex);
        }

        System.out.println("init finished");
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        Session session = HibernateUtil.getSessionFactory().openSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<CakeEntity> cr = cb.createQuery(CakeEntity.class);
        Root<CakeEntity> root = cr.from(CakeEntity.class);
        cr.select(root);

        List<CakeEntity> list = session.createQuery(cr).getResultList();
        resp.getWriter().println("[");

        for (CakeEntity entity : list) {
            resp.getWriter().println("\t{");

            resp.getWriter().println("\t\t\"title\" : " + entity.getTitle() + ", ");
            resp.getWriter().println("\t\t\"desc\" : " + entity.getDesc() + ",");
            resp.getWriter().println("\t\t\"image\" : " + entity.getImage());

            resp.getWriter().println("\t}");
        }

        resp.getWriter().println("]");

    }

}
