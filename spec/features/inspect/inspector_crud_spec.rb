describe "Inspectors", type: :feature do

  context 'as a system admin, ' do
    include_context :signed_in_as_a_system_admin

    context 'with an inspector config, ' do
      include_context :inspector_config

      example "I can create an inspector" do
          visit '/media-service/inspectors/'
          click_on 'Create'
          fill_in 'id', with: inspector_config[:id]
          check 'enabled'
          fill_in 'public_key', with: inspector_config[:'key-pair'][:'public-key']
          click_on 'Save'
          expect(page).to have_content 'Inspectors'
          expect(page).to have_content inspector_config[:id]

      end
    end
  end
end
