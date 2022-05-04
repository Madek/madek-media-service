describe "Inspectors", type: :feature do

  context 'as a system admin, ' do

    include_context :signed_in_as_a_system_admin

    context 'a configured inspector' do
      include_context :inspector

      it 'has been recently seen' do
        @inspector = inspector()
        wait_until(30) do
          visit '/media-service/inspectors/'
          page.has_content? 'less than a minute ago'
        end
      end

    end

  end
end
