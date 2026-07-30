(function(){
    'use strict';

    // ===== Form validate (Bootstrap) =====
    var forms = document.getElementsByClassName('needs-validation');
    Array.prototype.forEach.call(forms, function (form) {
        form.addEventListener('submit', function (e) {
            if (!form.checkValidity()) { e.preventDefault(); e.stopPropagation(); }
            form.classList.add('was-validated');
        }, false);
    });

    // ===== Role Picker =====
    var wrap   = document.getElementById('roleWrap');
    var search = document.getElementById('roleSearch');
    var count  = document.getElementById('roleCount');
    var chips  = document.getElementById('selectedChips');
    var btnAll = document.getElementById('btnSelectAll');
    var btnClr = document.getElementById('btnClear');

    if (!wrap) return; // page safety

    function updateCountAndChips(){
        var all = wrap.querySelectorAll('.pill input[type="checkbox"]');
        var checked = Array.prototype.filter.call(all, function(cb){ return cb.checked; });

        // count
        if (count) count.textContent = checked.length;

        // chips
        if (!chips) return;
        chips.innerHTML = '';
        if(checked.length === 0){
            chips.innerHTML = '<span class="soft">Chưa chọn role nào</span>';
            return;
        }
        checked.forEach(function(cb){
            var label = cb.closest('.pill').querySelector('.text').textContent;
            var id = cb.value;
            var span = document.createElement('span');
            span.className = 'chip';
            span.innerHTML = label + '<span class="close" data-id="'+id+'">&times;</span>';
            chips.appendChild(span);
        });
    }

    // toggle on pill click
    wrap.addEventListener('click', function(e){
        var pill = e.target.closest('.pill');
        if(!pill) return;

        // click dấu ×
        if(e.target.classList.contains('x')){
            var cbX = pill.querySelector('input[type="checkbox"]');
            cbX.checked = false;
            pill.classList.remove('active');
            updateCountAndChips();
            return;
        }

        var cb = pill.querySelector('input[type="checkbox"]');
        cb.checked = !cb.checked;
        pill.classList.toggle('active', cb.checked);
        updateCountAndChips();
    });

    // remove chip -> uncheck
    if (chips) {
        chips.addEventListener('click', function(e){
            if(!e.target.classList.contains('close')) return;
            var id = e.target.getAttribute('data-id');
            var input = wrap.querySelector('.pill input[value="'+id+'"]');
            if (!input) return;
            var pill = input.closest('.pill');
            input.checked = false;
            pill.classList.remove('active');
            updateCountAndChips();
        });
    }

    // search filter
    if (search) {
        search.addEventListener('input', function(){
            var q = search.value.trim().toLowerCase();
            var pills = wrap.querySelectorAll('.pill');
            pills.forEach(function(p){
                var text = p.querySelector('.text').textContent.toLowerCase();
                p.style.display = text.indexOf(q) > -1 ? 'inline-flex' : 'none';
            });
        });
    }

    // select all (only visible)
    if (btnAll) {
        btnAll.addEventListener('click', function(){
            wrap.querySelectorAll('.pill').forEach(function(p){
                if(p.style.display === 'none') return;
                var cb = p.querySelector('input[type="checkbox"]');
                cb.checked = true;
                p.classList.add('active');
            });
            updateCountAndChips();
        });
    }

    // clear all
    if (btnClr) {
        btnClr.addEventListener('click', function(){
            wrap.querySelectorAll('.pill input[type="checkbox"]').forEach(function(cb){
                cb.checked = false;
                cb.closest('.pill').classList.remove('active');
            });
            updateCountAndChips();
        });
    }

    // init: apply active class for pre-checked inputs + render chips
    wrap.querySelectorAll('.pill').forEach(function(p){
        var cb = p.querySelector('input[type="checkbox"]');
        if(cb.checked) p.classList.add('active');
    });
    updateCountAndChips();

})();
