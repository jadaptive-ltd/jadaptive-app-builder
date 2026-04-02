/**
 * Attaches to a text input, and also string values to be added to a list. The items
 * can be removed, but no-reordered or editor.
 * <p>
 * One pressing Enter, the item will be added to the list and the input cleared. If the
 * enter key is pressed while the input is empty, the usual form submission will be triggered.
 * 
 */
class CollectionTextFormInput {
    constructor(element) {
        this.container = element;
        this.input = this.selectOption('input.form-control');
		this.table = element.querySelector('[jad\\:role="table"]');
		this.tableItems = element.querySelector('[jad\\:role="items"]');
        this.initEvents();
    }
	
	addInputToTable() {
		var exists = false;
		this.table.querySelectorAll('tr input[type="hidden"]').forEach(el => {
			const thisValue = el.value;
			if(thisValue === this.input.value) {
				exists = true;
			}
		});

		if(exists) {
			return false;
		}
		else {
			this.table.querySelector('table').classList.remove('d-none');
			const tr = document.querySelector('[jad\\:role="item"]').content.cloneNode(true);
			const formVar = tr.querySelector('[jad\\:role="form-variable"]');
			formVar.name = this.input.dataset.formVariable;
			formVar.setAttribute('id',  this.input.dataset.formVariable);
			formVar.value = this.input.value;

			const displayName  = tr.querySelector('[jad\\:role="display-name"]');
			displayName.textContent = this.input.value;
			
			this.tableItems.appendChild(tr);
			
			return true;
		}
	}	
	
	initEvents() {

	    // Add to table when pressing enter, but only if the input is not empty. If the input is empty, allow form submission to happen as normal.
	    this.input.addEventListener('keydown', (e) => {
		    if (e.key === 'Enter') {
				if(this.input.value.trim() === '') {
					// Allow form submission if input is empty
				}
				else {
					e.preventDefault();
					if(this.addInputToTable()) {
						this.input.value = '';
					}
				}
			}
	    });
		
		// Add to table when pressing the add button, but only if the input is not empty.
        this.container.querySelector('.collectionTextAdd').addEventListener('click', (e) => {
            e.preventDefault();
            if (this.input.value.trim() === '') {
                // Do nothing if input is empty
            }
            else {
                if (this.addInputToTable()) {
                    this.input.value = '';
                }
            }
        });
						
		
		// Handlers for buttons when there is a table defined (up/down/delete)
		if(this.table) {
			this.table.addEventListener('click', (e) => {
	            if (e.target.closest('.collectionTextDelete')) {
	                e.preventDefault();
	                this.collectionTextDelete(e);
	            }
			});															
	    }			
	}

	collectionTextDelete(e) {
		e.target.closest('tr').remove();
		if(this.tableItems.children.length == 0) {
			this.table.querySelector('table').classList.add('d-none');
			this.container.querySelector('[jad\\:role="empty-table"]').classList.remove('d-none');
		}
		else {
			this.table.querySelector('table').classList.remove('d-none');
			this.container.querySelector('[jad\\:role="empty-table"]').classList.add('d-none');
		}
	}

	/* TODO make base class for form component and move this method there */
	selectOption(selector) {
		var el = this.container.querySelector(selector);
		if(!el) {
			throw new Error('Required element ' + selector + ' not found in DropdownFormInput');
		}
		return el;
	}
}

// Global initialization
document.querySelectorAll('.collection-text-field').forEach(el => new CollectionTextFormInput(el));