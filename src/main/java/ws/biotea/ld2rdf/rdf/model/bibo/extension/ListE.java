package ws.biotea.ld2rdf.rdf.model.bibo.extension;

import java.util.Collection;
import org.ontoware.rdf2go.model.Model;

public interface ListE<T> {
	public void addMember(Model model, T thing, boolean indexed);
	
	public void addMember(Model model, T thing, int index);

    public void addMembers(Model model, Collection<T> collection);
    
    public void addMembersInOrder(Model model, Collection<T> collection);
    
    public int size();
}
