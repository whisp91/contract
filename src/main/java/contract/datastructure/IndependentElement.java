package contract.datastructure;

import contract.assets.Const;
import contract.assets.Tools;
import contract.wrapper.Locator;
import contract.wrapper.Operation;
import contract.operation.OP_ReadWrite;
import contract.operation.OP_Swap;
import contract.operation.OperationType;

import java.util.Map;

/**
 * An independent variable holding a single element. May for example be used as a
 * temporary variable when performing a swap.
 *
 * @author Richard Sundqvist
 */
public class IndependentElement extends Array {

    /**
     * Version number for this class.
     */
    private static final long serialVersionUID = Const.VERSION_NUMBER;

    /**
     * Create a new IndependentElement.
     *
     * @param identifier The identifier for this IndependentElement.
     * @param abstractType The <b>raw</b> type of the element held by this IndependentElement.
     * @param visual The preferred visual style of the IndependentElement.
     */
    public IndependentElement (String identifier, AbstractType abstractType, VisualType visual,
                               Map<String, Object> attributes) {
        super(identifier, RawType.independentElement, abstractType, visual, attributes);
    }

    /**
     * Set the element held by this IndependentElement.
     *
     * @param newElement The new element to be held by this IndependentElement.
     */
    public void setElement (Element newElement) {
        elements.clear();
        elements.add(newElement);
    }

    /**
     * Initialize an element with value 0.
     *
     * @param value The value to initialize with.
     */
    public void initElement (double value) {
        Element init = new IndexedElement(value, new int[]{0});
        elements.clear();
        elements.add(init);
    }

    /**
     * Get the value held by the element contained in this IndependentElement.
     *
     * @return The value held by the element contained in this IndependentElement.
     */
    public double getNumericValue () {
        if (elements.isEmpty()) {
            return 0;
        }
        return elements.get(0).getNumValue();
    }

    @Override
    public void clear () {
        elements.clear();
        oc.reset();
        setRepaintAll(true);
    }

    @Override
    public void applyOperation (Operation op) {
        super.applyOperation(op);
        setRepaintAll(true);
    }

    @Override
    protected void executeSwap (OP_Swap op) {
        Element e = elements.get(0);
        if (op.getVar1().identifier.equals(identifier)) {
            e.setValue(op.getValue()[0]);
            e.count(OperationType.swap);
            oc.count(OperationType.swap);
            return;
        } else if (op.getVar2().identifier.equals(identifier)) {
            e.setValue(op.getValue()[1]);
            e.count(OperationType.swap);
            oc.count(OperationType.swap);
            return;
        }
    }

    @Override
    protected void executeRW (OP_ReadWrite op) {
        if (elements.isEmpty()) {
            initElement(op.getValue()[0]);
        }
        Element e = elements.get(0);
        if (op.getTarget() != null && op.getTarget().identifier.equals(identifier)) {
            e.setValue(op.getValue()[0]);
            modifiedElements.add(e);
            e.count(OperationType.write);
            oc.count(OperationType.write);
            return;
        } else if (op.getSource() != null && op.getSource().identifier.equals(identifier)) {
            modifiedElements.add(e);
            e.count(OperationType.read);
            oc.count(OperationType.read);
        }
    }

    @Override
    public VisualType resolveVisual () {
        setVisual(VisualType.single);
        return VisualType.single;
    }

    @Override
    public IndexedElement getElement (Locator locator) {
        if (locator == null) {
            return null;
        }

        if (locator.identifier.equals(identifier) && !elements.isEmpty()) {
            return (IndexedElement) elements.get(0);
        } else {
            return null;
        }
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder();
        sb.append("\"" + Tools.stripQualifiers(identifier) + "\": " + rawType);
        return sb.toString();
    }
}
