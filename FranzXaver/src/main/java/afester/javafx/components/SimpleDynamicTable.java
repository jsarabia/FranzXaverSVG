package afester.javafx.components;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Skin;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.layout.Pane;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.util.Callback;


public class SimpleDynamicTable extends TableView<IndexedTableRow<String>> {

    private ObservableList<IndexedTableRow<String>> data = FXCollections.observableArrayList();

    private boolean isShowHeader = true;
    private Pane rowHeader = null;
    private double hMinHeight = -1;
    private double hMaxHeight = -1;
    private double hPrefHeight = -1;
    private boolean hManaged = true;

    //private StringConverter<S> stringConverter;
    

    public SimpleDynamicTable(int columns, int rows) {

        // Note: getSelectedCells() returns a ObservableList<TablePosition> with a raw TablePosition
        getSelectionModel().getSelectedCells().addListener( (ListChangeListener.Change<? extends TablePosition> a) -> {
            TablePosition pos = a.getList().get(0);
            // pos.getRow();            The row number (int)
            // pos.getColumn();         The real column index as currently shown (if columns 0 and 1 are switched, column 1 still is at index 0) 
            // pos.getTableColumn();    The table column

            System.err.printf("CELL: %s/%s, %s%n", pos.getColumn() - 1, pos.getTableColumn().getId(), pos.getRow());
            
            this.edit(pos.getRow(), pos.getTableColumn());
            
        });

        setColumnCount(columns);
        setRowCount(rows);
        setShowColumnHeader(false);
        
        // set data for the table
        setItems(data);
        
        widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                if (rowHeader == null) {
                    // Get the table header
                    rowHeader = (Pane) lookup("TableHeaderRow");
                    toggleHeader();
                }
            }
        });
    }


    public void setValue(int column, int row, String value) {
        data.get(row).setValue(column, value);
    }

    public void setRowCount(int newValue) {
        if (newValue < data.size()) {
            data.remove(newValue, data.size());
        } else if (newValue > data.size()) {
            for (int rowIdx = data.size();  rowIdx < newValue;  rowIdx++) {
                IndexedTableRow<String> dataRow = new IndexedTableRow<>(rowIdx+1);
                
                data.add(dataRow);
            }
        }

        visibleRowCount.set(newValue);
    }

    public void setColumnCount(int newValue) {
        //newValue++; // first column is column header

        if (newValue < getColumns().size()) {
            getColumns().remove(newValue,  getColumns().size());
        } else if (newValue > getColumns().size()) {

            for (int colIdx = getColumns().size();  colIdx < newValue;  colIdx++) {
                    // TODO: at most 26
                    //String columnHeader = new String(Character.toChars('A' + colIdx));

                    TableColumn<IndexedTableRow<String>, String> column = new TableColumn<>(); // columnHeader);
                    column.setPrefWidth(100);
                    column.setId("" + colIdx);
                    column.setSortable(false);  // TODO: Make configurable from outside

                    column.setCellValueFactory(
                            new Callback<CellDataFeatures<IndexedTableRow<String>, String>, ObservableValue<String>>() {

                                @Override
                                public ObservableValue<String> call(CellDataFeatures<IndexedTableRow<String>, String> param) {
                                    IndexedTableRow<String> row = param.getValue();
                                    Integer colId = Integer.parseInt(param.getTableColumn().getId());
                                    String content = row.getValue(colId);
                                    if (content == null) {
                                        content = "";
                                    }
                                    return new ReadOnlyObjectWrapper<String>(content);
                                }

                            });

                    column.setCellFactory(LiveTextFieldTableCell.forTableColumn());

                    getColumns().add(column);
            }
        }
    }


    public void setShowColumnHeader(boolean flag) {
        this.isShowHeader = flag;
        toggleHeader();
    }

    private void toggleHeader() {
        // component not yet initialized, defer header update
        if (rowHeader == null) {
            return;
        }

        if(isShowHeader) {
            rowHeader.setMaxHeight(hMaxHeight);
            rowHeader.setMinHeight(hMinHeight);
            rowHeader.setPrefHeight(hPrefHeight);
            rowHeader.setManaged(hManaged);

            rowHeader.setVisible(true);
        } else {
            hMaxHeight = rowHeader.getMaxHeight();
            hMinHeight = rowHeader.getMinHeight();
            hManaged = rowHeader.isManaged();
            hPrefHeight = rowHeader.getPrefHeight();

            rowHeader.setMaxHeight(0);
            rowHeader.setMinHeight(0);
            rowHeader.setPrefHeight(0);
            rowHeader.setManaged(false);

            rowHeader.setVisible(false);
        }
    }
    
    // http://stackoverflow.com/questions/26298337/tableview-adjust-number-of-visible-rows/26364210#26364210
    private IntegerProperty visibleRowCount = new SimpleIntegerProperty(this, "visibleRowCount", 10);
    public IntegerProperty visibleRowCountProperty() {  return visibleRowCount; }


    public static class TableViewSkinY extends TableViewSkin<IndexedTableRow<String>> {

        public TableViewSkinY(SimpleDynamicTable tableView) {
            super(tableView);
            registerChangeListener(tableView.visibleRowCountProperty(), "VISIBLE_ROW_COUNT");
            handleControlPropertyChanged("VISIBLE_ROW_COUNT");
        }
        
        @Override
        protected void handleControlPropertyChanged(String p) {
            System.err.printf("handleControlPropertyChanged(%s)%n", p);

            super.handleControlPropertyChanged(p);
            if ("VISIBLE_ROW_COUNT".equals(p)) {
                needCellsReconfigured = true;
                getSkinnable().requestFocus();
            }
        }

        /**
         * Returns the visibleRowCount value of the table.
         */
        private int getVisibleRowCount() {
            int result = ((SimpleDynamicTable) getSkinnable()).visibleRowCountProperty().get();
            System.err.printf("getVisibleRowCount() = %s%n", result);
            
            return result;
        }

        /**
         * Reflectively invokes protected getCellLength(i) of flow.
         * @param index the index of the cell.
         * @return the cell height of the cell at index.
         */
        protected double invokeFlowCellLength(int index) {
            double height = 1.0;
            Class<?> clazz = VirtualFlow.class;
            try {
                Method method = clazz.getDeclaredMethod("getCellLength", Integer.TYPE);
                method.setAccessible(true);
                return ((double) method.invoke(flow, index));
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return height;
        }

        /**
         * Overridden to return custom flow.
         */
        @Override
        protected VirtualFlow createVirtualFlow() {
            return new MyFlow();
        }
        

        /**
         * Calculates and returns the pref height of the 
         * for the given number of rows.
         * 
         * If flow is of type MyFlow, queries the flow directly
         * otherwise invokes the method.
         */
        protected double getFlowPrefHeight(int rows) {
            double height = 0;
            if (flow instanceof MyFlow) {
                height = ((MyFlow) flow).getPrefLength(rows);
            }
            else {
                for (int i = 0; i < rows && i < getItemCount(); i++) {
                    height += invokeFlowCellLength(i);
                }
            }    
            return height + snappedTopInset() + snappedBottomInset();

        }

        /**
         * Overridden to compute the sum of the flow height and header prefHeight.
         */
        @Override
        protected double computePrefHeight(double width, double topInset,
                double rightInset, double bottomInset, double leftInset) {

            // super hard-codes to 400 .. doooh
            double prefHeight = getFlowPrefHeight(getVisibleRowCount());
            double result = prefHeight + getTableHeaderRow().prefHeight(width);
            System.err.printf("computePrefHeight() = %s%n", result);
            return result;
        }


        @Override
        protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset,
                double leftInset) {
            double result = computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
            System.err.printf("computeMaxHeight() = %s%n", result);
            return result;
        }


        /**
         * Extended to expose length calculation per a given # of rows.
         */
        public static class MyFlow extends VirtualFlow {

            protected double getPrefLength(int rowsPerPage) {
                double sum = 0.0;
                int rows = rowsPerPage; //Math.min(rowsPerPage, getCellCount());
                for (int i = 0; i < rows; i++) {
                    sum += getCellLength(i);
                }
                return sum;
            }

        }

    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new TableViewSkinY(this);
    }
}
